import { Delete, Edit } from '@navikt/ds-icons';
import AlertStripe from 'nav-frontend-alertstriper';
import { Fareknapp, Hovedknapp } from 'nav-frontend-knapper';
import NavFrontendSpinner from 'nav-frontend-spinner';
import React from 'react';
import { Row, Stack } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import FormInput from '../../components/form-elements/FormInput';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';
import { useInnsatsgrupper } from '../../hooks/tiltaksvariant/useInnsatsgrupper';
import './TiltaksvariantForm.less';
import { ReactComponent as AddCircle } from '../../ikoner/AddCircle.svg';
import { Select } from 'nav-frontend-skjema';

interface TiltaksvariantFormProps {
  isLoading?: boolean;
  isError?: boolean;
  tiltaksvariant?: Tiltaksvariant;
  onSubmit: (tiltaksvariant: Tiltaksvariant) => void;
  onDelete?: () => void;
  setModalOpen?: (open: boolean) => void;
}

const TiltaksvariantForm = ({ isLoading, isError, tiltaksvariant, onSubmit, onDelete }: TiltaksvariantFormProps) => {
  const { data: innsatsgrupper = [] } = useInnsatsgrupper();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Tiltaksvariant>({
    defaultValues: { innsatsgruppe: null, ...tiltaksvariant },
  });

  const tomtFeltErrorMessage = 'Dette feltet kan ikke være tomt';

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="rediger-opprett-tiltaksvariant__form"
      data-testid="form__rediger-opprett"
    >
      <Select label="Innsatsgruppe" data-testid="input_innsatsgruppe">
        <option value="">Velg innsatsgruppe</option>
        {innsatsgrupper.map(innsatsgruppe => (
          <option key={innsatsgruppe.id} value={innsatsgruppe.id}>
            {innsatsgruppe.tittel}
          </option>
        ))}
      </Select>
      <FormInput
        label="Tittel"
        register={register('tittel', {
          required: tomtFeltErrorMessage,
          maxLength: { value: 50, message: 'Maks 50 tegn.' },
        })}
        defaultValue={tiltaksvariant ? tiltaksvariant.tittel : ''}
        feil={errors.tittel && errors.tittel.message}
        className="rediger-opprett-tiltaksvariant__form__tittel"
        data-testid="input_tittel"
      />
      <FormInput
        label="Ingress"
        register={register('ingress', {
          required: tomtFeltErrorMessage,
          maxLength: { value: 250, message: 'Maks 250 tegn.' },
        })}
        defaultValue={tiltaksvariant ? tiltaksvariant.ingress : ''}
        feil={errors.ingress && errors.ingress.message}
        className="rediger-opprett-tiltaksvariant__form__ingress"
        data-testid="input_ingress"
      />
      <FormInput
        label="Beskrivelse"
        register={register('beskrivelse', { required: tomtFeltErrorMessage })}
        defaultValue={tiltaksvariant ? tiltaksvariant.beskrivelse : ''}
        feil={errors.beskrivelse && errors.beskrivelse.message}
        className="rediger-opprett-tiltaksvariant__form__beskrivelse"
        data-testid="input_beskrivelse"
      />
      <Row className="knapperad">
        <Stack direction="horizontal" gap={2}>
          {onDelete ? (
            <>
              <Hovedknapp htmlType="submit" data-testid="submit-knapp_rediger-tiltaksvariant">
                Rediger tiltaksvariant <Edit title="Blyant" />
              </Hovedknapp>
              <Fareknapp
                spinner={isLoading}
                onClick={onDelete}
                htmlType="button"
                data-testid="slett-knapp_rediger-tiltaksvariant"
              >
                Slett tiltaksvariant <Delete title="Søppelkasse" />
              </Fareknapp>
            </>
          ) : (
            <Hovedknapp htmlType="submit" data-testid="submit-knapp_opprett-tiltaksvariant">
              Opprett tiltaksvariant <AddCircle />
            </Hovedknapp>
          )}
        </Stack>
      </Row>
      {isLoading && <NavFrontendSpinner />}
      {isError && <AlertStripe type="feil">Det har oppstått en feil</AlertStripe>}
    </form>
  );
};

export default TiltaksvariantForm;
