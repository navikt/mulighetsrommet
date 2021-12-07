import { AddCircle, Delete, Edit } from '@navikt/ds-icons';
import AlertStripe from 'nav-frontend-alertstriper';
import { Fareknapp, Hovedknapp } from 'nav-frontend-knapper';
import NavFrontendSpinner from 'nav-frontend-spinner';
import React from 'react';
import { Row, Stack } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import FormInput from '../../components/form-elements/FormInput';
import { Select } from '../../components/form-elements/Select';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';
import { useInnsatsgrupper } from '../../hooks/tiltaksvariant/useInnsatsgrupper';
import './TiltaksvariantForm.less';

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
    control,
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
      <Select
        name="innsatsgruppe"
        control={control}
        nullable
        label="Innsatsgruppe"
        placeholder="Ingen innsatsgruppe"
        options={innsatsgrupper.map(innsatsgruppe => ({ value: innsatsgruppe.id, label: innsatsgruppe.tittel }))}
      />
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
                Rediger tiltaksvariant <Edit />
              </Hovedknapp>
              <Fareknapp
                spinner={isLoading}
                onClick={onDelete}
                htmlType="button"
                data-testid="slett-knapp_rediger-tiltaksvariant"
              >
                Slett tiltaksvariant <Delete />
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
