import { headers } from './headers';
type Dataset = 'production' | 'test' | 'dev';

interface SanityHttpResponse<T> {
  ms: number;
  query: string;
  result: T;
}

class Client {
  accessToken: String;
  mock: boolean = import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === 'true';
  useCdn: boolean = process.env.NODE_ENV === 'production';

  constructor(accessToken: string) {
    if (!accessToken && this.mock) throw new Error('You must provide an access token to connect to Sanity');
    this.accessToken = accessToken;
  }

  async query<T>(query: string, dataset: Dataset = 'production'): Promise<SanityHttpResponse<T>> {
    if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK) {
      headers.append('Authorization', `Bearer ${this.accessToken}`);
    }
    const url = this.createSanityUrl(this.useCdn, this.mock, dataset, query);
    const response = await fetch(url, {
      headers,
    });
    if (!response.ok) {
      throw new Error('Could not fetch from Sanity', {
        cause: new Error(response.statusText),
      });
    }

    return response.json() as Promise<SanityHttpResponse<T>>;
  }

  private createSanityUrl(useCdn: boolean, isMockEnabled: boolean, dataset: Dataset, query: string): string {
    if (isMockEnabled) {
      return `https://xegcworx.${
        useCdn ? 'apicdn' : 'api'
      }.sanity.io/v2022-06-08/data/query/${dataset}?query=${encodeURIComponent(query)}`;
    } else {
      const apiBase = String(import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? '');
      return `${apiBase}/api/v1/sanity?query=${encodeURIComponent(query)}&dataset=${dataset}`;
    }
  }
}

const client = new Client(String(import.meta.env.VITE_SANITY_ACCESS_TOKEN ?? ''));

export { client };
