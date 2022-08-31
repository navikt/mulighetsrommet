import { MulighetsrommetClient } from 'mulighetsrommet-api-client';

export const mulighetsrommetClient = new MulighetsrommetClient({
  BASE: import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? '',
});
