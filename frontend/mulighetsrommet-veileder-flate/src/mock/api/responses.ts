import { context, response } from 'msw';

export function ok<T>(data?: T) {
  return response(context.delay(), context.status(200), data ? context.json(data) : context.text('OK'));
}

export function notFound<T>(data?: T) {
  return response(context.delay(), context.status(404), data ? context.json(data) : context.text('Not found'));
}

export function badReq<T>(data?: T) {
  return response(context.delay(), context.status(400), data ? context.json(data) : context.text('Bad request'));
}
