import { useSanity } from './useSanity';
import {StatistikkFil} from "../models";

export default function useSisteStatistikkFil() {
  return useSanity<StatistikkFil>(`*[_type == "statistikkfil"]| order(_createdAt desc) {
        _id,
        "statistikkFilUrl": statistikkfilopplastning.asset->url,
}[0]`);
}
