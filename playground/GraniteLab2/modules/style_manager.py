import streamlit as st

def load_custom_css():
    st.markdown("""
        <style>
        /* Custom CSS can go here */
        </style>
    """, unsafe_allow_html=True)
