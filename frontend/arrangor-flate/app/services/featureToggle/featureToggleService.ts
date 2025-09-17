import { FeatureToggleService, Tiltakskode, FeatureToggle } from "@api-client";

interface Props {
  orgnr: string;
  feature: FeatureToggle;
  tiltakskoder: Tiltakskode[];
  headers: Record<string, string>;
}

export async function toggleIsEnabled(params: Props): Promise<boolean> {
  const { orgnr, feature, tiltakskoder, headers } = params;
  const res = await FeatureToggleService.getFeatureToggle({
    path: { orgnr },
    query: { feature, tiltakskoder },
    headers,
  });
  if (!res.data) {
    return false;
  }

  return res.data;
}
