import { useState } from 'react';
import {
    Container, Box, TextField, Button, Typography,
    Paper, Alert, CircularProgress
} from '@mui/material';
import { loadStripe } from '@stripe/stripe-js';
import {
    Elements, CardElement, useStripe, useElements
} from '@stripe/react-stripe-js';
import { createPayment, PaymentResponse } from '../api';
import { v4 as uuidv4 } from 'uuid';

const stripePromise = loadStripe('pk_test_51UCncl0zT1FbLMnYlns4d8Qpli79C2KvXC9SlRfguffZFKYk5pAmNjtU8GZDdTR9HeyrijsceXI5w8jXK7k9Hmzg00zoyiMZyZ');

const CheckoutForm = () => {
    const stripe = useStripe();
    const elements = useElements();
    const [amount, setAmount] = useState('25.00');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [lastPayment, setLastPayment] = useState<PaymentResponse | null>(null);
    const [idempotencyKey] = useState(uuidv4());
    const [retryCount, setRetryCount] = useState(0);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!stripe || !elements) return;

        setLoading(true);
        setError('');

        try {
            const payment = await createPayment(
                {
                    amount: parseFloat(amount),
                    currency: 'usd',
                    customerEmail: email,
                },
                idempotencyKey
            );

            setLastPayment(payment);

            if (!payment.clientSecret) {
                // No clientSecret means this came back from the idempotency cache,
                // not a fresh Stripe call — proves the duplicate was caught
                setLoading(false);
                return;
            }

            const cardElement = elements.getElement(CardElement);
            if (!cardElement) return;

            const result = await stripe.confirmCardPayment(payment.clientSecret, {
                payment_method: { card: cardElement },
            });

            if (result.error) {
                setError(result.error.message || 'Payment failed');
            }
        } catch (err: any) {
            setError(err.response?.data?.message || 'Payment failed');
        } finally {
            setLoading(false);
        }
    };

    const handleRetrySameKey = async () => {
        setLoading(true);
        setError('');
        setRetryCount(retryCount + 1);

        try {
            const payment = await createPayment(
                {
                    amount: parseFloat(amount),
                    currency: 'usd',
                    customerEmail: email,
                },
                idempotencyKey // same key on purpose
            );
            setLastPayment(payment);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Retry failed');
        } finally {
            setLoading(false);
        }
    };

    if (lastPayment) {
        const wasFromCache = !lastPayment.clientSecret;
        return (
            <Box>
                <Alert severity={wasFromCache ? 'info' : 'success'} sx={{ mb: 2 }}>
                    {wasFromCache
                        ? `Duplicate request caught — returned cached payment #${lastPayment.id} instead of calling Stripe again.`
                        : `Payment successful — payment #${lastPayment.id} created.`}
                </Alert>

                <Typography variant="body2" sx={{ mb: 1 }}>
                    Payment ID: {lastPayment.id}
                </Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>
                    Stripe PaymentIntent: {lastPayment.stripePaymentIntentId}
                </Typography>
                <Typography variant="body2" sx={{ mb: 2 }}>
                    Status: {lastPayment.status}
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                    Idempotency Key (unchanged): {idempotencyKey}
                </Typography>

                <Button
                    fullWidth
                    variant="outlined"
                    onClick={handleRetrySameKey}
                    disabled={loading}
                >
                    {loading ? <CircularProgress size={24} /> : `Resend same request (retry #${retryCount + 1})`}
                </Button>
            </Box>
        );
    }

    return (
        <Box component="form" onSubmit={handleSubmit}>
            <TextField
                fullWidth
                label="Amount (USD)"
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                margin="normal"
            />
            <TextField
                fullWidth
                label="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                margin="normal"
            />
            <Box sx={{ p: 2, border: '1px solid #ccc', borderRadius: 1, my: 2 }}>
                <CardElement />
            </Box>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                Idempotency Key: {idempotencyKey}
            </Typography>
            <Button
                fullWidth
                type="submit"
                variant="contained"
                disabled={!stripe || loading}
            >
                {loading ? <CircularProgress size={24} /> : 'Pay'}
            </Button>
        </Box>
    );
};

const CheckoutPage = () => {
    return (
        <Container maxWidth="sm" sx={{ mt: 8 }}>
            <Paper elevation={3} sx={{ p: 4 }}>
                <Typography variant="h4" sx={{ mb: 3, textAlign: 'center', fontWeight: 'bold' }}>
                    Sentry Pay
                </Typography>
                <Elements stripe={stripePromise}>
                    <CheckoutForm />
                </Elements>
            </Paper>
        </Container>
    );
};

export default CheckoutPage;