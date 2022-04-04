import { Features, ALERT_INFO, FAKE_DOOR } from '../../api/feature-toggles';

export const mockFeatures: Features = {
  [ALERT_INFO]: true,
  [FAKE_DOOR]: false,
};
