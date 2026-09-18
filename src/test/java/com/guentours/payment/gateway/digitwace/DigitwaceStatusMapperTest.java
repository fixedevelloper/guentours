package com.guentours.payment.gateway.digitwace;

import com.guentours.payment.gateway.ChargeStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DigitwaceStatusMapperTest {

    @Test
    void mapsKnownSuccessValuesCaseInsensitively() {
        assertThat(DigitwaceStatusMapper.map("success")).isEqualTo(ChargeStatus.SUCCEEDED);
        assertThat(DigitwaceStatusMapper.map("SUCCESSFUL")).isEqualTo(ChargeStatus.SUCCEEDED);
        assertThat(DigitwaceStatusMapper.map("Completed")).isEqualTo(ChargeStatus.SUCCEEDED);
    }

    @Test
    void mapsKnownFailureValuesCaseInsensitively() {
        assertThat(DigitwaceStatusMapper.map("failed")).isEqualTo(ChargeStatus.FAILED);
        assertThat(DigitwaceStatusMapper.map("DECLINED")).isEqualTo(ChargeStatus.FAILED);
        assertThat(DigitwaceStatusMapper.map("Cancelled")).isEqualTo(ChargeStatus.FAILED);
    }

    /**
     * An unrecognized/null status must never resolve to FAILED: WacePay PayIn's real status
     * vocabulary isn't confirmed yet (see DigitwaceProperties), and wrongly failing a payment that
     * actually went through needs a manual refund to fix, whereas PENDING just waits for the next
     * status check.
     */
    @Test
    void treatsUnknownOrMissingStatusAsPendingRatherThanFailed() {
        assertThat(DigitwaceStatusMapper.map("some_unexpected_value")).isEqualTo(ChargeStatus.PENDING);
        assertThat(DigitwaceStatusMapper.map(null)).isEqualTo(ChargeStatus.PENDING);
    }
}
