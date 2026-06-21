package com.tianji.api.client.activity;

import com.tianji.api.client.activity.fallback.ActivityRecipientClientFallback;
import com.tianji.api.dto.activity.ActivityRegistrationRecipientDTO;
import com.tianji.api.dto.activity.CompetitionRegistrationRecipientDTO;
import com.tianji.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(contextId = "activityRecipient", value = "rk-activity", fallbackFactory = ActivityRecipientClientFallback.class)
public interface ActivityRecipientClient {

    @GetMapping("/api/activity/{id}/registrations")
    R<List<ActivityRegistrationRecipientDTO>> getActivityRegistrations(@PathVariable("id") Long id);

    @GetMapping("/api/competition/{id}/registrations")
    R<List<CompetitionRegistrationRecipientDTO>> getCompetitionRegistrations(@PathVariable("id") Long id);
}
