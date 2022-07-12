import {useSendEventTilApi} from "./queries/useSendEventTilApi";

export const logEvent = (logTag: string, fields?: {}, tags?: {}): void => {
  if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === 'true') {
    console.log('Event', logTag, 'Fields:', fields, 'Tags:', tags);
  } else {
    useSendEventTilApi({name: logTag, fields: fields, tags: tags});
  }
};
