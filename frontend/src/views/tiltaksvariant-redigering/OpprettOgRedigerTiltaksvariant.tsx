import React, { useState } from 'react';
import './OpprettOgRedigerTiltaksvariant.less';
import { useParams } from 'react-router-dom';
import SlettModal from '../../components/modal/SlettModal';
import MainView from '../../layouts/MainView';
import useTiltaksvariantCreate from '../../hooks/tiltaksvariant/useTiltaksvariantCreate';
import useTiltaksvariantUpdate from '../../hooks/tiltaksvariant/useTiltaksvariantUpdate';
import useTiltaksvariantDelete from '../../hooks/tiltaksvariant/useTiltaksvariantDelete';
import useTiltaksvariant from '../../hooks/tiltaksvariant/useTiltaksvariant';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';
import TiltaksvariantForm from './TiltaksvariantForm';

interface routeParams {
  id: string;
}

const OpprettOgRedigerTiltaksvariant = () => {
  const { id }: routeParams = useParams();
  const isEditMode = !!id;

  const [isModalOpen, setIsModalOpen] = useState(false);

  const { data, isLoading, isError } = useTiltaksvariant(id);

  const postMutation = useTiltaksvariantCreate();
  const putMutation = useTiltaksvariantUpdate(id);
  const deleteMutation = useTiltaksvariantDelete(id);

  const handleSubmit = (tiltaksvariant: Tiltaksvariant) => {
    isEditMode ? putMutation.mutate(tiltaksvariant) : postMutation.mutate(tiltaksvariant);
  };

  const getTitle = isEditMode ? 'Rediger tiltaksvariant' : 'Opprett tiltaksvariant';

  return (
    <MainView
      title={getTitle}
      dataTestId={isEditMode ? 'header-rediger-tiltaksvariant' : 'header_opprett-tiltaksvariant'}
      tilbakelenke={isEditMode ? '/tiltaksvarianter/' + id : '/'}
    >
      <div>
        <TiltaksvariantForm
          isLoading={isLoading}
          isError={isError}
          onSubmit={handleSubmit}
          setModalOpen={setIsModalOpen}
          tiltaksvariant={data}
        />
      </div>
      <SlettModal
        tittel="Slett tiltaksvariant"
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        handleDelete={deleteMutation.mutate}
      />
    </MainView>
  );
};

export default OpprettOgRedigerTiltaksvariant;
