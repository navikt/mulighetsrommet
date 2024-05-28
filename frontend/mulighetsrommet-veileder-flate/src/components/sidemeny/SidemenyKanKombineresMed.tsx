import { Box } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./SidemenyInfo.module.scss";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

export const SidemenyKanKombineresMed = ({ tiltaksgjennomforing }: Props) => {
  const { tiltakstype } = tiltaksgjennomforing;

  console.log(tiltakstype);
  console.log(tiltakstype.kanKombineresMed?.map((tiltakstypen) => ({ tiltakstypen })));

  return (
    <Box padding="5" background="bg-subtle" className={styles.panel} id="sidemeny">
      <ul>
        {tiltakstype.kanKombineresMed?.map((tiltakstypen) => (
          <li key={tiltakstype.sanityId}>{tiltakstypen}</li>
        ))}
      </ul>
      {/*<Regelverksinfo*/}
      {/*  regelverkLenker={[*/}
      {/*    ...regelverkLenker,*/}
      {/*    {*/}
      {/*      _id: "klage",*/}
      {/*      regelverkLenkeNavn: "Avslag og klage",*/}
      {/*      regelverkUrl:*/}
      {/*        "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Klage-p%C3%A5-arbeidsmarkedstiltak.aspx",*/}
      {/*    },*/}
      {/*    {*/}
      {/*      _id: "vurdering",*/}
      {/*      regelverkLenkeNavn: "Tiltak hos familie/nærstående",*/}
      {/*      regelverkUrl:*/}
      {/*        "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Rutine.aspx",*/}
      {/*    },*/}
      {/*  ]}*/}
      {/*/>*/}
      {/*</div>*/}
      {/*)}*/}
    </Box>
  );
};
