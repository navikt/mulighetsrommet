import { useSendEventTilApi } from './queries/useSendEventTilApi';
import { erPreview } from '../../utils/Utils';

export const logEvent = (logTag: string, fields?: {}, tags?: {}): void => {
  if (!erPreview) {
    useSendEventTilApi({ name: logTag, fields, tags });
  } else if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === 'true' || erPreview) {
    // eslint-disable-next-line no-console
    console.log('Event', logTag, 'Fields:', fields, 'Tags:', tags);
  }
};
