import { environments } from '../env';

export function lenkeTilOpprettAvtaleForEnv(): string {
  const env: environments = import.meta.env.VITE_ENVIRONMENT;
  const baseUrl =
    env === 'production'
      ? 'https://tiltaksgjennomforing.intern.nav.no/'
      : 'https://tiltaksgjennomforing.dev.intern.nav.no/';
  return `${baseUrl}tiltaksgjennomforing/opprett-avtale`;
}
