import { headers } from './headers';
import { useQuery } from 'react-query';

export const ENABLE_ARBEIDSFLATE = 'mulighetsrommet.enable-arbeidsflate';
export const FEEDBACK = 'mulighetsrommet.feedback';
export const VIS_HISTORIKK = 'mulighetsrommet.vis-historikk';
export const VIS_INNSIKTSFANE = 'mulighetsrommet.vis-innsiktsfane';
export const VIS_TILGJENGELIGHETSSTATUS = 'mulighetsrommet.vis-tilgjengelighetsstatus';

export const ALL_TOGGLES = [ENABLE_ARBEIDSFLATE, FEEDBACK, VIS_HISTORIKK, VIS_INNSIKTSFANE, VIS_TILGJENGELIGHETSSTATUS];

export interface Features {
  [ENABLE_ARBEIDSFLATE]: boolean;
  [FEEDBACK]: boolean;
  [VIS_HISTORIKK]: boolean;
  [VIS_INNSIKTSFANE]: boolean;
  [VIS_TILGJENGELIGHETSSTATUS]: boolean;
}

export const initialFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: false,
  [FEEDBACK]: false,
  [VIS_HISTORIKK]: false,
  [VIS_INNSIKTSFANE]: false,
  [VIS_TILGJENGELIGHETSSTATUS]: false,
};

const toggles = ALL_TOGGLES.map(element => 'feature=' + element).join('&');
export const fetchConfig = {
  headers,
};

export const useFeatureToggles = () => {
  return useQuery<Features>(['features'], () =>
    fetch(`/veilarbpersonflatefs/api/feature?${toggles}`, fetchConfig).then(Response => {
      return Response.ok ? Response.json() : initialFeatures;
    })
  );
};
