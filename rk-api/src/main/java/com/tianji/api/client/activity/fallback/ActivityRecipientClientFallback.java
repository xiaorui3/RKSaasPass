package com.tianji.api.client.activity.fallback;

import com.tianji.api.client.activity.ActivityRecipientClient;
import com.tianji.api.dto.activity.ActivityRegistrationRecipientDTO;
import com.tianji.api.dto.activity.CompetitionRegistrationRecipientDTO;
import com.tianji.common.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ActivityRecipientClientFallback implements FallbackFactory<ActivityRecipientClient> {
    @Override
    public ActivityRecipientClient create(Throwable cause) {
        log.error("查询活动/比赛报名收件人失败", cause);
        return new ActivityRecipientClient() {
            @Override
            public R<List<ActivityRegistrationRecipientDTO>> getActivityRegistrations(Long id) {
                return R.ok(Collections.emptyList());
            }

            @Override
            public R<List<CompetitionRegistrationRecipientDTO>> getCompetitionRegistrations(Long id) {
                return R.ok(Collections.emptyList());
            }
        };
    }
}
