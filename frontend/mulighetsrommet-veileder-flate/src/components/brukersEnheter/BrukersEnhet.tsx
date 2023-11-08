import { Tag } from "@navikt/ds-react";
import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import {
  brukersGeografiskeOgOppfolgingsenhetErLokalkontorMenIkkeSammeKontor,
  kebabCase,
} from "../../utils/Utils";

export function BrukersEnhet() {
  const brukerdata = useHentBrukerdata();
  const brukersOppfolgingsenhet = brukerdata?.data?.oppfolgingsenhet;

  if (brukerdata?.isLoading || !brukerdata.data) {
    return null;
  }

  if (brukersGeografiskeOgOppfolgingsenhetErLokalkontorMenIkkeSammeKontor(brukerdata.data)) {
    return (
      <Tag
        className="cypress-tag"
        key={"navenhet"}
        size="small"
        data-testid={`${kebabCase("filtertag_oppfolgingsnavenhet")}`}
        title="Brukers oppfølgingsenhet"
        variant="info"
      >
        {brukersOppfolgingsenhet?.navn}
      </Tag>
    );
  } else {
    return (
      <Tag
        className="cypress-tag"
        key={"navenhet"}
        size="small"
        data-testid={`${kebabCase("filtertag_oppfolgingsnavenhet")}`}
        title="Brukers oppfølgingsenhet"
        variant="info"
      >
        {brukerdata?.data.geografiskEnhet.navn}
      </Tag>
    );
  }
}
