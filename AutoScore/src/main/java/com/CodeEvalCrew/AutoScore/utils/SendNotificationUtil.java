package com.CodeEvalCrew.AutoScore.utils;

import org.springframework.stereotype.Component;

import com.CodeEvalCrew.AutoScore.models.Entity.Account;
import com.CodeEvalCrew.AutoScore.models.Entity.Accout_Notification;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.Notification_Status_Enum;
import com.CodeEvalCrew.AutoScore.models.Entity.Notification;
import com.CodeEvalCrew.AutoScore.repositories.account_repository.IAccountRepository;
import com.CodeEvalCrew.AutoScore.repositories.notification_repository.AccountNotificationRepository;

@Component
public class SendNotificationUtil {
    private final AccountNotificationRepository accNotiRepo;
    private final IAccountRepository accountRepository;

    public SendNotificationUtil(
        AccountNotificationRepository accNotiRepo,
        IAccountRepository accountRepository
    ){
        this.accNotiRepo = accNotiRepo;
        this.accountRepository = accountRepository;
    }

    public void sendNotification(Notification noti, Long accountId){    
        Account acc = accountRepository.findById(accountId).get();
        Accout_Notification accNotification = new Accout_Notification(accountId, Notification_Status_Enum.UNREAD, acc, noti);
        accNotiRepo.save(accNotification);
    }
}
