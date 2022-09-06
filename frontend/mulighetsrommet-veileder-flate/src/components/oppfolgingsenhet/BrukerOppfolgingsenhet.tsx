import { Tag, Alert } from '@navikt/ds-react';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { kebabCase } from '../../utils/Utils';

export function BrukersOppfolgingsenhet() {
  const brukerdata = useHentBrukerdata();
  const brukersOppfolgingsenhet = brukerdata?.data?.oppfolgingsenhet?.navn;

  if (brukerdata?.isLoading) {
    return null;
  }

  return brukersOppfolgingsenhet ? (
    <Tag
      className="nav-enhet-tag cypress-tag"
      key={'navenhet'}
      variant={brukersOppfolgingsenhet ? 'info' : 'error'}
      size="small"
      data-testid={`${kebabCase('filtertag_navenhet')}`}
      title="Brukers oppfølgingsenhet"
    >
      {brukersOppfolgingsenhet}
    </Tag>
  ) : (
    <Alert
      title="Kontroller om brukeren er under oppfølging og finnes i Arena"
      key="alert-navenhet"
      data-testid="alert-navenhet"
      size="small"
      variant="error"
    >
      Fant ingen oppfølgingsenhet
    </Alert>
  );
}
