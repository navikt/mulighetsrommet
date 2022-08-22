import { FAKE_DOOR, Features, FEEDBACK } from '../../core/api/feature-toggles';

export const mockFeatures: Features = {
  [FAKE_DOOR]: false,
  [FEEDBACK]: true,
};
