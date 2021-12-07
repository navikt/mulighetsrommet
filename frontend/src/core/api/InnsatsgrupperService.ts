import { Innsatsgruppe } from '../domain/Innsatsgruppe';
import { api } from './ApiUtils';

const getInnsatsgrupper = () => api<Innsatsgruppe[]>('/innsatsgrupper', { method: 'GET' });

export const InnsatsgrupperService = {
  getInnsatsgrupper,
};
