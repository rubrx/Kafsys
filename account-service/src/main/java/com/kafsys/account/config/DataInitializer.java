package com.kafsys.account.config;

import com.kafsys.account.entity.Account;
import com.kafsys.account.repository.AccountRepository;
import com.kafsys.common.enums.AccountStatus;
import com.kafsys.common.enums.KycStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AccountRepository accountRepository;

    public DataInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountRepository.count() == 0) {
            seedAccounts();
        }
    }

    private void seedAccounts() {
        String[][] seedData = {
            {"user-alice-001", "Alice Chen",    "50000.00", "USD"},
            {"user-bob-001",   "Bob Taylor",    "35000.00", "USD"},
            {"user-carol-001", "Carol Smith",   "75000.00", "USD"},
            {"user-dave-001",  "Dave Johnson",  "12000.00", "USD"},
            {"user-eve-001",   "Eve Martinez",  "90000.00", "GBP"},
        };

        for (String[] seed : seedData) {
            Account account = new Account();
            account.setOwnerId(seed[0]);
            account.setOwnerName(seed[1]);
            account.setBalance(new BigDecimal(seed[2]));
            account.setCurrency(seed[3]);
            account.setStatus(AccountStatus.ACTIVE);
            account.setKycStatus(KycStatus.VERIFIED);
            accountRepository.save(account);
        }

        log.info("Seeded {} accounts", seedData.length);
    }
}
