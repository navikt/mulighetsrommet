export const ALERT_INFO = 'mulighetsrommet.alert-info';
export const FAKE_DOOR = 'mulighetsrommet.fake-door';

export const ALL_TOGGLES = [ALERT_INFO, FAKE_DOOR];

export interface Features {
  [ALERT_INFO]: boolean;
  [FAKE_DOOR]: boolean;
}
export const initialFeatures: Features = {
  [ALERT_INFO]: true,
  [FAKE_DOOR]: false,
};
