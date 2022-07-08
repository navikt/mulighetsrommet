import {FrontendEvent, MulighetsrommetService} from 'mulighetsrommet-api-client';

export function useSendEventTilApi(event: FrontendEvent) {
  MulighetsrommetService.logFrontendEvent({requestBody: event});
}
