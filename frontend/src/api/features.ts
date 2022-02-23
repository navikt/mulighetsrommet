export const TILTAKSTYPELISTE = 'mulighetsrommet.tiltakstypeliste';

export const ALL_TOGGLES = [TILTAKSTYPELISTE];

export interface Features {
  [TILTAKSTYPELISTE]: boolean;
}
export const initialFeatures: Features = {
  [TILTAKSTYPELISTE]: false,
};

//TODO brukes på denne måten, og den delen som skal være i en feature-toggle skal være wrappet med {tiltaksliste}.
//Spør Tania hvis noe er uklart! Fjern også denne kommentaren ved første feature-toggle
// const features = useFetchFeatureToggle();
// const tiltaksliste = features[TILTAKSTYPELISTE];
