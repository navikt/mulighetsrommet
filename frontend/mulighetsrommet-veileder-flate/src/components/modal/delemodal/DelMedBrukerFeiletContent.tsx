import { ErrorColored } from '@navikt/ds-icons';
import { Heading, BodyShort, Button } from '@navikt/ds-react';
import classNames from 'classnames';
import { Dispatch } from 'react';
import Lenke from '../../lenke/Lenke';
import modalStyles from '../Modal.module.scss';
import delemodalStyles from './Delemodal.module.scss';
import { Actions } from './DelemodalActions';

interface Props {
  dispatch: Dispatch<Actions>;
  onCancel: () => void;
}

export function DelMedBrukerFeiletContent({ dispatch, onCancel }: Props) {
  return (
    <div className={classNames(delemodalStyles.delemodal_tilbakemelding)}>
      <ErrorColored className={delemodalStyles.delemodal_svg} />
      <Heading level="1" size="large" data-testid="modal_header">
        Tiltaket kunne ikke deles med brukeren
      </Heading>
      <BodyShort>
        Vi kunne ikke dele informasjon digitalt med denne brukeren. Dette kan være fordi hen ikke ønsker eller kan
        benytte de digitale tjenestene våre.{' '}
        <Lenke
          isExternal
          to="https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-arbeidsrettet-brukeroppfolging/SitePages/Manuell-oppf%C3%B8lging-i-Modia-arbeidsrettet-oppf%C3%B8lging.aspx"
        >
          Les mer om manuell oppfølging{' '}
        </Lenke>
      </BodyShort>
      <div className={modalStyles.modal_btngroup}>
        <Button variant="primary" onClick={() => dispatch({ type: 'Reset' })} data-testid="modal_btn-reset">
          Prøv på nytt
        </Button>
        <Button variant="secondary" onClick={onCancel} data-testid="modal_btn-cancel">
          Lukk
        </Button>
      </div>
    </div>
  );
}
