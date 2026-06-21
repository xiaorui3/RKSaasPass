package com.tianji.user.service;

import com.tianji.api.dto.user.EmailVerificationSendDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;

public interface IEmailVerificationService {

    boolean sendCode(EmailVerificationSendDTO dto);

    boolean verifyCode(EmailVerificationVerifyDTO dto);
}
