package fm.lossless.auth.service.social;

public interface SocialTokenVerifier {

    SocialProvider provider();

    SocialProviderProfile verify(String providerToken);
}
