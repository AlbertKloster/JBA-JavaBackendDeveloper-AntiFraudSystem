package antifraud;

import antifraud.dto.PutAccessDto;
import antifraud.dto.PutRoleDto;
import antifraud.dto.PutTransactionDto;
import antifraud.dto.TransactionResponseDto;
import antifraud.entity.*;
import antifraud.service.*;
import antifraud.enums.Operation;
import antifraud.enums.Result;
import antifraud.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class Controller {

    private final AmountLimit INITIAL_LIMIT = new AmountLimit(1L, 200L, 1500L);

    private final UserService userService;
    private final SuspiciousIdService suspiciousIdService;
    private final StolenCardService stolenCardService;
    private final TransactionService transactionService;
    private final AmountLimitService amountLimitService;

    @Autowired
    public Controller(UserService userService,
                      SuspiciousIdService suspiciousIdService,
                      StolenCardService stolenCardService,
                      TransactionService transactionService,
                      AmountLimitService amountLimitService) {
        this.userService = userService;
        this.suspiciousIdService = suspiciousIdService;
        this.stolenCardService = stolenCardService;
        this.transactionService = transactionService;
        this.amountLimitService = amountLimitService;
    }

    @PostMapping(value = "/api/antifraud/transaction", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionResponseDto> postTransaction(@Valid @RequestBody Transaction transaction) {
        TransactionResponseDto transactionResponseDto = new TransactionResponseDto();
        checkTransaction(transaction, transactionResponseDto);
        saveTransaction(transaction, transactionResponseDto);
        return new ResponseEntity<>(transactionResponseDto, HttpStatus.OK);
    }

    private void saveTransaction(Transaction transaction, TransactionResponseDto transactionResponseDto) {
        transaction.setResult(transactionResponseDto.getResult());
        transaction.setFeedback("");
        transactionService.save(transaction);
    }

    private void checkTransaction(Transaction transaction, TransactionResponseDto transactionResponseDto) {
        checkAmount(transactionResponseDto, transaction.getAmount());
        checkStolenCard(transactionResponseDto, transaction.getNumber());
        checkSuspiciousIp(transactionResponseDto, transaction.getIp());
        checkTransactionsInLastHour(transaction, transactionResponseDto);
    }

    private void checkTransactionsInLastHour(Transaction transaction, TransactionResponseDto transactionResponseDto) {
        List<Transaction> transactionsInLastHourForCardNumber =
                transactionService.findAllByNumberAndDateBetween(transaction.getNumber(), transaction.getDate());
        checkRegions(transactionResponseDto, transactionsInLastHourForCardNumber, transaction.getRegion());
        checkIps(transactionResponseDto, transactionsInLastHourForCardNumber, transaction.getIp());
    }

    private void checkAmount(TransactionResponseDto response, Long amount) {
        AmountLimit amountLimit = amountLimitService.get().orElse(INITIAL_LIMIT);
        if (amount > amountLimit.getMaxManual())
            setProhibited(response);
        else if (amount > amountLimit.getMaxAllowed())
            setManual(response);
        else
            setAllowed(response);
    }

    private void setProhibited(TransactionResponseDto response) {
        response.setResult(Result.PROHIBITED.name());
        response.setInfo("amount");
    }

    private void setManual(TransactionResponseDto response) {
        response.setResult(Result.MANUAL_PROCESSING.name());
        response.setInfo("amount");
    }

    private void setAllowed(TransactionResponseDto response) {
        response.setResult(Result.ALLOWED.name());
        response.setInfo("none");
    }

    private void checkSuspiciousIp(TransactionResponseDto response, String ip) {
        if (suspiciousIdService.findSuspiciousIpByIp(ip).isPresent())
            if (response.getResult().equals(Result.PROHIBITED.name()))
                response.setInfo(response.getInfo() + ", ip");
            else
                setProhibitedIp(response);
    }

    private void setProhibitedIp(TransactionResponseDto response) {
        response.setResult(Result.PROHIBITED.name());
        response.setInfo("ip");
    }

    private void checkStolenCard(TransactionResponseDto response, String number) {
        if (stolenCardService.findStolenCardByNumber(number).isPresent())
            if (response.getResult().equals(Result.PROHIBITED.name()))
                response.setInfo(response.getInfo() + ", card-number");
            else
                setProhibitedCardNumber(response);
    }

    private void setProhibitedCardNumber(TransactionResponseDto response) {
        response.setResult(Result.PROHIBITED.name());
        response.setInfo("card-number");
    }

    private void checkRegions(TransactionResponseDto response,
                              List<Transaction> transactionsInLastHourForCardNumber,
                              String region) {
        int diffRegions = transactionsInLastHourForCardNumber.stream()
                .map(Transaction::getRegion)
                .filter(transactionRegion -> !transactionRegion.equals(region))
                .collect(Collectors.toSet()).size();

        if (diffRegions < 2)
            return;

        if (diffRegions > 2) {
            setResponseProhibited(response, "region-correlation");
            return;
        }

        if (response.getResult().equals(Result.PROHIBITED.name()))
            return;

        if (response.getResult().equals(Result.MANUAL_PROCESSING.name()))
            response.setInfo(response.getInfo() + ", region-correlation");
        else
            setResultAndInfo(response, Result.MANUAL_PROCESSING.name(), "region-correlation");
    }

     private void checkIps(TransactionResponseDto response,
                              List<Transaction> transactionsInLastHourForCardNumber,
                              String ip) {
        int diffIps = transactionsInLastHourForCardNumber.stream()
                .map(Transaction::getIp)
                .filter(transactionIp -> !transactionIp.equals(ip))
                .collect(Collectors.toSet()).size();

        if (diffIps < 2)
            return;

        if (diffIps > 2) {
            setResponseProhibited(response, "ip-correlation");
            return;
        }

        if (response.getResult().equals(Result.PROHIBITED.name()))
            return;

        if (response.getResult().equals(Result.MANUAL_PROCESSING.name()))
            response.setInfo(response.getInfo() + ", ip-correlation");
        else
            setResultAndInfo(response, Result.MANUAL_PROCESSING.name(), "ip-correlation");
    }

    private void setResponseProhibited(TransactionResponseDto response, String info) {
        if (response.getResult().equals(Result.PROHIBITED.name()))
            response.setInfo(response.getInfo() + ", " + info);
        else
            setResultAndInfo(response, Result.PROHIBITED.name(), info);
    }

    private void setResultAndInfo(TransactionResponseDto response, String result, String info) {
        response.setResult(result);
        response.setInfo(info);
    }

    @PostMapping(value = "/api/auth/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> postUser(@Valid @RequestBody User user) {
        return new ResponseEntity<>(userService.register(user), HttpStatus.CREATED);

    }

    @GetMapping(value = "/api/auth/list")
    public ResponseEntity<List<Map<String, Object>>> getList() {
        return new ResponseEntity<>(userService.getList(), HttpStatus.OK);
    }

    @DeleteMapping(value = "/api/auth/user/{username}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String username) {
        return new ResponseEntity<>(userService.delete(username), HttpStatus.OK);
    }

    @PutMapping(value = "/api/auth/role")
    public ResponseEntity<Map<String, Object>> putRole(@RequestBody PutRoleDto putRoleDto) {
        User user = userService.findUserByUsernameIgnoreCase(putRoleDto.getUsername());
        setRole(user, putRoleDto);
        return new ResponseEntity<>(userService.update(user), HttpStatus.OK);
    }

    private void setRole(User user, PutRoleDto putRoleDto) {
        if (hasEqualRoles(user, putRoleDto))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        user.setRole(Role.getRole(putRoleDto.getRole()).getRoleWithPrefix());
    }

    private boolean hasEqualRoles(User user, PutRoleDto putRoleDto) {
        return user.getRole().replaceAll("ROLE_", "").equals(putRoleDto.getRole());
    }

    @PutMapping(value = "/api/auth/access")
    public ResponseEntity<Map<String, Object>> putAccess(@RequestBody PutAccessDto putAccessDto) {
        User user = userService.findUserByUsernameIgnoreCase(putAccessDto.getUsername());
        setAccess(user, putAccessDto);
        String status = putAccessDto.getOperation().equals("UNLOCK") ? "unlocked" : "locked";
        return new ResponseEntity<>(Map.of("status", String.format("User %s %s!", putAccessDto.getUsername(), status)), HttpStatus.OK);
    }

    private void setAccess(User user, PutAccessDto putAccessDto) {
        if (user.getRole().equals(Role.ADMINISTRATOR.getRoleWithPrefix()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        user.setAccountNonLocked(putAccessDto.getOperation().equals(Operation.UNLOCK.name()));
        userService.update(user);
    }

    @PostMapping(value = "/api/antifraud/suspicious-ip", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuspiciousIp> postSuspiciousIp(@Valid @RequestBody SuspiciousIp suspiciousIp) {
        return new ResponseEntity<>(suspiciousIdService.save(suspiciousIp), HttpStatus.OK);
    }

    @DeleteMapping(value = "/api/antifraud/suspicious-ip/{ip}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteSuspiciousIp(@PathVariable String ip) {
        return new ResponseEntity<>(suspiciousIdService.delete(ip), HttpStatus.OK);
    }

    @GetMapping(value = "/api/antifraud/suspicious-ip", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SuspiciousIp>> getSuspiciousIps() {
        return new ResponseEntity<>(suspiciousIdService.findAll(), HttpStatus.OK);
    }

    @PostMapping(value = "/api/antifraud/stolencard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StolenCard> postStolenCard(@Valid @RequestBody StolenCard stolenCard) {
        if (isNotValidNumber(stolenCard.getNumber()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(stolenCardService.save(stolenCard), HttpStatus.OK);
    }

    @DeleteMapping(value = "/api/antifraud/stolencard/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteStolenCard(@PathVariable String number) {
        if (isNotValidNumber(number))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(stolenCardService.delete(number), HttpStatus.OK);
    }

    @GetMapping(value = "/api/antifraud/stolencard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StolenCard>> getStolenCards() {
        return new ResponseEntity<>(stolenCardService.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "/api/antifraud/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Transaction>> getHistory() {
        return new ResponseEntity<>(transactionService.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "/api/antifraud/history/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Transaction>> getHistoryNumber(@PathVariable String number) {
        if (isNotValidNumber(number))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(transactionService.findAllByNumber(number), HttpStatus.OK);
    }

    @PutMapping(value = "/api/antifraud/transaction", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Transaction> putTransaction(@Valid @RequestBody PutTransactionDto putTransactionDto) {
        Transaction transaction = transactionService.findById(putTransactionDto.getTransactionId());
        checkTransaction(transaction, putTransactionDto);
        updateLimits(transaction, putTransactionDto);
        transaction.setFeedback(putTransactionDto.getFeedback());
        return new ResponseEntity<>(transactionService.save(transaction), HttpStatus.OK);
    }

    private void checkTransaction(Transaction transaction, PutTransactionDto putTransactionDto) {
        checkResult(putTransactionDto);
        checkValidityFeedbackException(transaction, putTransactionDto);
        checkFeedbackAlreadyExist(transaction);
    }

    private void checkResult(PutTransactionDto putTransactionDto) {
        if (Result.isNotValidResult(putTransactionDto.getFeedback()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    private void checkValidityFeedbackException(Transaction transaction, PutTransactionDto putTransactionDto) {
        if (transaction.getResult().equals(putTransactionDto.getFeedback()))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private void checkFeedbackAlreadyExist(Transaction transaction) {
        if (!transaction.getFeedback().isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT);
    }

    private void updateLimits(Transaction transaction, PutTransactionDto putTransactionDto) {
        checkAndUpdateLimit(transaction.getAmount(), transaction.getResult(), putTransactionDto.getFeedback());
    }

    private void checkAndUpdateLimit(Long amount, String result, String feedback) {
        checkIncMaxAllowed(amount, result, feedback);
        checkIncMaxManual(amount, result, feedback);
        checkDecMaxAllowed(amount, result, feedback);
        checkDecMaxManual(amount, result, feedback);
    }

    private void checkIncMaxAllowed(Long amount, String result, String feedback) {
        if ((result.equals((Result.MANUAL_PROCESSING.name())) || result.equals((Result.PROHIBITED.name())))
                && feedback.equals(Result.ALLOWED.name()))
            incMaxAllowed(amount);
    }

    private void checkIncMaxManual(Long amount, String result, String feedback) {
        if ((feedback.equals((Result.ALLOWED.name())) || feedback.equals((Result.MANUAL_PROCESSING.name())))
                && result.equals(Result.PROHIBITED.name()))
            incMaxManual(amount);
    }

    private void checkDecMaxAllowed(Long amount, String result, String feedback) {
        if ((feedback.equals((Result.MANUAL_PROCESSING.name())) || feedback.equals((Result.PROHIBITED.name())))
                && result.equals(Result.ALLOWED.name()))
            decMaxAllowed(amount);
    }

    private void checkDecMaxManual(Long amount, String result, String feedback) {
        if ((result.equals((Result.ALLOWED.name())) || result.equals((Result.MANUAL_PROCESSING.name())))
                && feedback.equals(Result.PROHIBITED.name()))
            decMaxManual(amount);
    }

    private void incMaxAllowed(Long amount) {
        AmountLimit amountLimit = amountLimitService.get().orElse(INITIAL_LIMIT);
        amountLimit.setMaxAllowed(incLimit(amountLimit.getMaxAllowed(), amount));
        amountLimitService.save(amountLimit);
    }

    private void decMaxAllowed(Long amount) {
        AmountLimit amountLimit = amountLimitService.get().orElse(INITIAL_LIMIT);
        amountLimit.setMaxAllowed(decLimit(amountLimit.getMaxAllowed(), amount));
        amountLimitService.save(amountLimit);
    }

    private void incMaxManual(Long amount) {
        AmountLimit amountLimit = amountLimitService.get().orElse(INITIAL_LIMIT);
        amountLimit.setMaxManual(incLimit(amountLimit.getMaxManual(), amount));
        amountLimitService.save(amountLimit);
    }

    private void decMaxManual(Long amount) {
        AmountLimit amountLimit = amountLimitService.get().orElse(INITIAL_LIMIT);
        amountLimit.setMaxManual(decLimit(amountLimit.getMaxManual(), amount));
        amountLimitService.save(amountLimit);
    }

    private Long incLimit(Long limit, Long amount) {
        return (long) Math.ceil(0.8 * limit + 0.2 * amount);
    }

    private Long decLimit(Long limit, Long amount) {
        return (long) Math.ceil(0.8 * limit - 0.2 * amount);
    }

    private boolean isNotValidNumber(String number) {
        if (!number.matches("\\d{16}"))
            return true;
        int[] digits = Arrays.stream(number.split("")).mapToInt(Integer::parseInt).toArray();
        int sum = digits[digits.length - 1];
        int parity = (digits.length - 2) % 2;
        for (int i = 0 ; i < digits.length - 1; i++) {
            int digit = digits[i];
            if (i % 2 == parity)
                digit *= 2;
            if (digit > 9)
                digit -= 9;
            sum += digit;
        }
        return sum % 10 != 0;
    }

}
