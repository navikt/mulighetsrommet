export const ALERT_INFO = 'mulighetsrommet.alert-info';

export const ALL_TOGGLES = [ALERT_INFO];

export interface Features {
  [ALERT_INFO]: boolean;
}
export const initialFeatures: Features = {
  [ALERT_INFO]: false,
};

//TODO brukes på denne måten, og den delen som skal være i en feature-toggle skal være wrappet med {tiltaksliste}.
//Spør Tania hvis noe er uklart! Fjern også denne kommentaren ved første feature-toggle
// const features = useFetchFeatureToggle();
// const tiltaksliste = features[TILTAKSTYPELISTE];
