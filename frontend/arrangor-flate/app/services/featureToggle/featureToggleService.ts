import { FeatureToggleArrangorService, Tiltakskode, Toggles } from "../../../api-client";

interface Props {
  orgnr: string;
  feature: Toggles;
  tiltakskoder: Tiltakskode[];
  headers: Record<string, string>;
}

export async function toggleIsEnabled(params: Props): Promise<boolean> {
  const { orgnr, feature, tiltakskoder, headers } = params;
  const res = await FeatureToggleArrangorService.getFeatureToggleArrangor({
    path: { orgnr },
    query: { feature, tiltakskoder },
    headers,
  });
  if (!res.data) {
    return false;
  }

  return res.data;
}
