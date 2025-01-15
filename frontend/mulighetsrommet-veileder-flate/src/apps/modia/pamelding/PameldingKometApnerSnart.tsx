import { useFeatureToggle } from "@/api/feature-toggles";
import { TegnestiftIkon } from "@/ikoner/TegnestiftIkon";
import { Toggles, VeilederflateTiltak } from "@mr/api-client";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { BodyShort, GuidePanel, Heading, VStack } from "@navikt/ds-react";
import styles from "./PameldingKometApnerSnart.module.scss";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function PameldingKometApnerSnart({ tiltak }: Props) {
  const tiltakskoder = tiltak.tiltakstype?.tiltakskode ? [tiltak.tiltakstype.tiltakskode] : [];
  const { data: pameldingApnerSnart } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_PAMELDING_INFO_KOMET,
    tiltakskoder,
  );

  if (!pameldingApnerSnart) {
    return null;
  }

  return (
    <GuidePanel
      className={styles.overwritten_poster}
      illustration={<TegnestiftIkon aria-label="Tegnestifikon" />}
    >
      <VStack gap="3">
        <Heading level="4" size="small" className={styles.text_center}>
          Flere tiltak får ny påmeldingsløsning i Modia
        </Heading>
        <BodyShort>
          Påmelding for digitalt jobbsøkerkurs og VTA skal i fremtiden gjøres her i Modia. Ny
          løsning blir tilgjengelig 10. desember.
        </BodyShort>
        <BodyShort>
          <Lenke
            target="_blank"
            rel="noreferrer noopener"
            to="https://navno.sharepoint.com/sites/intranett-produktomrader-og-prosjekter/SitePages/Ny-l%C3%B8sning-for-p%C3%A5melding-til-arbeidsforberedende-trening.aspx"
          >
            Les mer på navet her
          </Lenke>
        </BodyShort>
      </VStack>
    </GuidePanel>
  );
}
