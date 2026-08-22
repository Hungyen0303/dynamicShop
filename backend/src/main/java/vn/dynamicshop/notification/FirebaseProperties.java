package vn.dynamicshop.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.firebase.*} — xem {@link FirebaseFcmConfig}. Mặc định tắt (Stage 0 behavior). */
@ConfigurationProperties(prefix = "app.firebase")
public class FirebaseProperties {

    private boolean enabled = false;
    private String serviceAccountPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }
}
