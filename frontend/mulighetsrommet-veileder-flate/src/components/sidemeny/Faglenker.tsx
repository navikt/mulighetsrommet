import { RedaksjoneltInnholdLenke } from "@arbeidsmarkedstiltak/api-client";
import { Link, VStack } from "@navikt/ds-react";

interface FaglenkerProps {
  faglenker?: RedaksjoneltInnholdLenke[];
}

export function Faglenker({ faglenker }: FaglenkerProps) {
  if (!faglenker) {
    return null;
  }
  const generelleLenker = [
    {
      id: "avslag-og-klage",
      navn: "Avslag og klage",
      url: "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Klage-p%C3%A5-arbeidsmarkedstiltak.aspx",
      beskrivelse: null,
    },
    {
      id: "tiltak-hos-familie",
      navn: "Tiltak hos familie/nærstående",
      url: "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Rutine.aspx",
      beskrivelse: null,
    },
  ];
  return (
    <VStack gap="space-4" align="end">
      {faglenker.map((lenke) => (
        <Faglenke key={lenke.url} lenke={lenke} />
      ))}
      {generelleLenker.map((lenke) => (
        <Faglenke key={lenke.url} lenke={lenke} />
      ))}
    </VStack>
  );
}

interface RegelverklenkeProps {
  lenke: RedaksjoneltInnholdLenke;
}

function Faglenke({ lenke }: RegelverklenkeProps) {
  if (!lenke.url) {
    return null;
  }

  return (
    <Link target="_blank" href={lenke.url} key={lenke.url} className="text-right">
      {lenke.navn}{" "}
    </Link>
  );
}
