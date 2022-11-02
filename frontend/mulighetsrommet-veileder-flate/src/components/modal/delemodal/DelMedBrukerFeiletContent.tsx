import { Button } from '@navikt/ds-react';
import classNames from 'classnames';
import { Dispatch } from 'react';
import modalStyles from '../Modal.module.scss';
import delemodalStyles from './Delemodal.module.scss';
import { Actions } from './DelemodalActions';
import { Feilmelding } from '../../feilmelding/Feilmelding';

interface Props {
  dispatch: Dispatch<Actions>;
  onCancel: () => void;
}

export function DelMedBrukerFeiletContent({ dispatch, onCancel }: Props) {
  return (
    <div className={classNames(delemodalStyles.delemodal_tilbakemelding)}>
      <div style={{ padding: '0 5rem' }}>
        <Feilmelding
          ikonvariant={'error'}
          header={<>Tiltaket kunne ikke deles</>}
          beskrivelse={
            <>
              Tiltaket kunne ikke deles på grunn av en teknisk feil hos oss. <a href=".">Forsøk på nytt</a> eller
              ta&nbsp;
              <a href="https://jira.adeo.no/plugins/servlet/desk/portal/541/create/4442">kontakt</a> i Porten dersom du
              trenger mer hjelp.
            </>
          }
          medContainer={false}
        />
      </div>

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
