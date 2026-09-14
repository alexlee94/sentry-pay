import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8082',
    headers: {
        'Content-Type': 'application/json',
    },
});

export interface CreatePaymentRequest {
    amount: number;
    currency: string;
    customerEmail?: string;
}

export interface PaymentResponse {
    id: number;
    stripePaymentIntentId: string;
    amount: number;
    currency: string;
    status: string;
    clientSecret: string | null;
}

export const createPayment = async (
    request: CreatePaymentRequest,
    idempotencyKey: string
): Promise<PaymentResponse> => {
    const response = await api.post('/api/payments', request, {
        headers: { 'Idempotency-Key': idempotencyKey },
    });
    return response.data;
};

export default api;