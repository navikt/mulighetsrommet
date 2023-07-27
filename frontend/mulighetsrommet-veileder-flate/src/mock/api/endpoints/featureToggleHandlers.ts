import { rest } from 'msw';
import invariant from 'tiny-invariant';
import { Features } from '../../../core/api/feature-toggles';
import { mockFeatures } from '../features';

export const featureToggleHandlers = [
  rest.get('*/api/v1/internal/features', (req, res, ctx) => {
    const feature = req.url.searchParams.get('feature') as keyof Features;
    invariant(feature, 'Feature er ikke satt');
    return res(ctx.status(200), ctx.json(mockFeatures[feature]));
  }),
];
