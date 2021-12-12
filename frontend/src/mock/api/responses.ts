import { context, response } from 'msw';

/**
 * MSW REST response creators
 */
export const Responses = {
  ok,
  notFound,
};

export function ok<T>(data?: T) {
  return response(context.delay(), context.status(200), data ? context.json(data) : context.text('OK'));
}

export function notFound<T>(data?: T) {
  return response(context.delay(), context.status(404), data ? context.json(data) : context.text('Not found'));
}
