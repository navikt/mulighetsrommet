import { headers } from './headers';

type Dataset = 'production' | 'test' | 'dev';

interface SanityHttpResponse<T> {
  ms: number;
  query: string;
  result: T;
}

class Client {
  headers: Headers;
  mock: boolean = import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === 'true';

  constructor(accessToken: string) {
    if (!accessToken && this.mock) {
      throw new Error('You must provide an access token to connect to Sanity');
    }

    this.headers = new Headers(headers);

    if (accessToken) {
      this.headers.delete('Authorization');
      this.headers.append('Authorization', `Bearer ${accessToken}`);
    }
  }

  async query<T>(query: string, dataset: Dataset = 'production'): Promise<SanityHttpResponse<T>> {
    const url = this.createSanityUrl(dataset, query);
    const response = await fetch(url, {
      headers: this.headers,
    });
    if (!response.ok) {
      throw new Error('Could not fetch from Sanity', {
        cause: new Error(response.statusText),
      });
    }

    return response.json() as Promise<SanityHttpResponse<T>>;
  }

  private createSanityUrl(dataset: Dataset, query: string): string {
    if (this.mock) {
      return `${import.meta.env.VITE_SANITY_API_URL}/data/query/${dataset}?query=${encodeURIComponent(query)}`;
    } else {
      const apiBase = String(import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? '');
      return `${apiBase}/api/v1/sanity?query=${encodeURIComponent(query)}&dataset=${dataset}`;
    }
  }
}

const client = new Client(String(import.meta.env.VITE_SANITY_ACCESS_TOKEN ?? ''));

export { client };
