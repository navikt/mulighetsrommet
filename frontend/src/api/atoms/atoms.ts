import { atom } from 'jotai';
import { Tiltaksvariant } from '../models/Tiltaksvariant';

export const filtrerteTiltaksvarianter = atom<Tiltaksvariant[]>([]);
export const tiltaksvariantOversiktSok = atom('');
export const filtreringInnsatsgruppe = atom<number[]>([]);
