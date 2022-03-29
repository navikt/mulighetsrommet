export const ALERT_INFO = 'mulighetsrommet.alert-info';

export const ALL_TOGGLES = [ALERT_INFO];

export interface Features {
  [ALERT_INFO]: boolean;
}
export const initialFeatures: Features = {
  [ALERT_INFO]: true,
};
