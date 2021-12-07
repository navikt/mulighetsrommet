import { Id } from '../domain/Generic';
import { Tiltaksvariant } from '../domain/Tiltaksvariant';
import { api } from './ApiUtils';

const getAllTiltaksvarianter = () => api<Tiltaksvariant[]>('/tiltaksvarianter', { method: 'GET' });

const getTiltaksvariantById = (id: Id) => api<Tiltaksvariant>(`/tiltaksvarianter/${id}`, { method: 'GET' });

const postTiltaksvariant = (tiltaksvariant: Tiltaksvariant) =>
  api<Tiltaksvariant>('/tiltaksvarianter', { method: 'POST', body: JSON.stringify(tiltaksvariant) });

const putTiltaksvariant = (tiltaksvariant: Tiltaksvariant) =>
  api<Tiltaksvariant>(`/tiltaksvarianter/${tiltaksvariant.id}`, {
    method: 'PUT',
    body: JSON.stringify(tiltaksvariant),
  });

const deleteTiltaksvariant = (id: Id) => api<Tiltaksvariant>(`/tiltaksvarianter/${id}`, { method: 'DELETE' });

const TiltaksvariantService = {
  getAllTiltaksvarianter,
  getTiltaksvariantById,
  postTiltaksvariant,
  putTiltaksvariant,
  deleteTiltaksvariant,
};

export default TiltaksvariantService;
