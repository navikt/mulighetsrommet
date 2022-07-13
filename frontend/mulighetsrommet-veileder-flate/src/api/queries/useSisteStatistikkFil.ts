import { useSanity } from './useSanity';
import { StatistikkFil } from '../models';
import groq from 'groq';

export default function useSisteStatistikkFil() {
  return useSanity<StatistikkFil>(groq`*[_type == "statistikkfil"]| order(_createdAt desc) {
        _id,
        "statistikkFilUrl": statistikkfilopplastning.asset->url,
}[0]`);
}
