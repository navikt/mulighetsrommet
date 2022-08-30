import { rest, RestHandler } from 'msw';
import { mockFeatures } from './features';

export const veilarbpersonflateHandlers: RestHandler[] = [
  rest.get('*/api/feature', (req, res, ctx) => {
    return res(ctx.delay(500), ctx.json(mockFeatures));
  }),
];

export const veilarbdialogHandlers: RestHandler[] = [
  rest.post('*/api/dialog', (req, res, ctx) => {
    return res(ctx.delay(500), ctx.json({ id: '1234' }));
  }),
];
