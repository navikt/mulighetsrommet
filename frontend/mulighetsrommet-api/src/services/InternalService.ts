/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class InternalService {

    /**
     * @returns string API is online and responding
     * @throws ApiError
     */
    public static getInternalPing(): CancelablePromise<'PONG'> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/internal/ping',
        });
    }

}