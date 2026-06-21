package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.AdmissionFormConfigDTO;
import com.tianji.user.service.IAdmissionFormConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionFormConfigControllerTest {

    @Mock
    private IAdmissionFormConfigService admissionFormConfigService;

    @InjectMocks
    private AdmissionFormConfigController controller;

    @Test
    void getPublicConfig_shouldUseExplicitTenantIdInsteadOfAmbientTenantContext() {
        AdmissionFormConfigDTO expected = new AdmissionFormConfigDTO();
        when(admissionFormConfigService.getTenantConfig(2L)).thenReturn(expected);

        R<AdmissionFormConfigDTO> response = controller.getPublicConfig(2L);

        verify(admissionFormConfigService).getTenantConfig(2L);
        assertSame(expected, response.getData());
    }
}
