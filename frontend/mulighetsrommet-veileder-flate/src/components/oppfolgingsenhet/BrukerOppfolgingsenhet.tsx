import { Tag, Alert } from '@navikt/ds-react';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { kebabCase } from '../../utils/Utils';
import { ErrorTag } from '../tags/ErrorTag';

export function BrukersOppfolgingsenhet() {
  const brukerdata = useHentBrukerdata();
  const brukersOppfolgingsenhet = brukerdata?.data?.oppfolgingsenhet?.navn;

  if (brukerdata?.isLoading) {
    return null;
  }

  return brukersOppfolgingsenhet ? (
    <Tag
      className="cypress-tag"
      key={'navenhet'}
      size="small"
      data-testid={`${kebabCase('filtertag_navenhet')}`}
      title="Brukers oppfølgingsenhet"
      variant={'info'}
    >
      {brukersOppfolgingsenhet}
    </Tag>
  ) : (
    <ErrorTag innhold={'Enhet mangler'} title={'Kontroller om brukeren er under oppfølging og finnes i Arena'} />
  );
}
