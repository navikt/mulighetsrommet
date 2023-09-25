import { Tag } from "@navikt/ds-react";
import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import { kebabCase } from "../../utils/Utils";
import { ErrorTag } from "../tags/ErrorTag";

export function BrukersGeografiskeEnhet() {
  const brukerdata = useHentBrukerdata();
  const brukersGeografiskeEnhet = brukerdata?.data?.geografiskEnhet?.navn;

  if (brukerdata?.isLoading) {
    return null;
  }

  return brukersGeografiskeEnhet ? (
    <Tag
      className="cypress-tag"
      key={"navenhet"}
      size="small"
      data-testid={`${kebabCase("filtertag_navenhet")}`}
      title="Brukers geografiske enhet"
      variant={"info"}
    >
      {brukersGeografiskeEnhet}
    </Tag>
  ) : (
    <ErrorTag
      innhold={"Enhet mangler"}
      title={"Kontroller om brukeren er under oppfølging og finnes i Arena"}
      dataTestId={"alert-navenhet"}
    />
  );
}
